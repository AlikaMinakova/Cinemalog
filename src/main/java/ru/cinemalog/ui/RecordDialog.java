package ru.cinemalog.ui;

import ru.cinemalog.application.ValidationException;
import ru.cinemalog.domain.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;

public final class RecordDialog extends JDialog {
    public record Result(String title, ContentType type, Integer year, Genre genre, Status status, Integer rating, String comment, SeriesProgress progress) {}
    private final JTextField title = new JTextField(28); private final JComboBox<ContentType> type = new JComboBox<>(ContentType.values()); private final JTextField year = new JTextField(8);
    private final JComboBox<Genre> genre = new JComboBox<>(Genre.values()); private final JComboBox<Status> status = new JComboBox<>(Status.values()); private final JComboBox<Integer> rating = new JComboBox<>(); private final JTextArea comment = new JTextArea(5,28);
    private final JPanel seriesPanel = new JPanel(new GridBagLayout()); private final JTextField totalSeasons = new JTextField(5); private final JTextField watchedSeasons = new JTextField(5); private final JPanel seasonsRows = new JPanel();
    private final JLabel error = new JLabel(" "); private Result result;
    public RecordDialog(Window owner, String titleText, MovieRecord initial) {
        super(owner,titleText,ModalityType.APPLICATION_MODAL); setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); setSize(620,650); setLocationRelativeTo(owner);
        for(int i=1;i<=10;i++)rating.addItem(i); rating.setSelectedItem(null); status.setSelectedItem(Status.WANT_TO_WATCH); build(initial);
    }
    private void build(MovieRecord initial){
        JPanel root=new JPanel(new BorderLayout(8,8));root.setBorder(new EmptyBorder(10,10,10,10));
        JPanel form=new JPanel(new GridBagLayout()); GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(4,4,4,4);g.anchor=GridBagConstraints.WEST;g.fill=GridBagConstraints.HORIZONTAL;
        int r=0; add(form,g,r++,"Название*",title);add(form,g,r++,"Тип контента*",type);add(form,g,r++,"Год выпуска",year);add(form,g,r++,"Жанр",genre);add(form,g,r++,"Статус*",status);add(form,g,r++,"Оценка",rating);add(form,g,r++,"Комментарий",new JScrollPane(comment));
        seriesPanel.setBorder(BorderFactory.createTitledBorder("Данные сериала")); seriesPanel.setVisible(false); form.add(seriesPanel,grid(g,0,r++,2,1));
        type.addActionListener(e->rebuildSeries());
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));JButton cancel=new JButton("Отмена"), save=new JButton("Сохранить");buttons.add(cancel);buttons.add(save);cancel.addActionListener(e->{result=null;dispose();});save.addActionListener(e->submit());
        error.setForeground(new Color(170,0,0)); root.add(form,BorderLayout.CENTER);root.add(error,BorderLayout.SOUTH);root.add(buttons,BorderLayout.PAGE_END);setContentPane(root);
        if(initial!=null) fill(initial); rebuildSeries();
    }
    private GridBagConstraints grid(GridBagConstraints src,int x,int y,int w,int h){GridBagConstraints g=(GridBagConstraints)src.clone();g.gridx=x;g.gridy=y;g.gridwidth=w;g.gridheight=h;return g;}
    private void add(JPanel p,GridBagConstraints base,int row,String label,Component c){p.add(new JLabel(label),grid(base,0,row,1,1));p.add(c,grid(base,1,row,1,1));}
    private void fill(MovieRecord r){title.setText(r.title());type.setSelectedItem(r.contentType());if(r.releaseYear()!=null)year.setText(r.releaseYear().toString());genre.setSelectedItem(r.genre());status.setSelectedItem(r.status());rating.setSelectedItem(r.rating());comment.setText(r.comment());if(r.seriesProgress()!=null){totalSeasons.setText(String.valueOf(r.seriesProgress().totalSeasons()));watchedSeasons.setText(String.valueOf(r.seriesProgress().watchedSeasons()));}}
    private void rebuildSeries(){seriesPanel.removeAll();if(type.getSelectedItem()!=ContentType.SERIES){seriesPanel.setVisible(false);pack();return;}seriesPanel.setVisible(true);GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(3,3,3,3);g.anchor=GridBagConstraints.WEST;seriesPanel.add(new JLabel("Всего сезонов*"),grid(g,0,0,1,1));seriesPanel.add(totalSeasons,grid(g,1,0,1,1));seriesPanel.add(new JLabel("Просмотрено сезонов"),grid(g,2,0,1,1));seriesPanel.add(watchedSeasons,grid(g,3,0,1,1));JButton apply=new JButton("Создать поля сезонов");apply.addActionListener(e->buildSeasonRows());seriesPanel.add(apply,grid(g,0,1,4,1));seasonsRows.setLayout(new BoxLayout(seasonsRows,BoxLayout.Y_AXIS));seriesPanel.add(new JScrollPane(seasonsRows),grid(g,0,2,4,1));pack();}
    private void buildSeasonRows(){seasonsRows.removeAll();try{int n=Integer.parseInt(totalSeasons.getText());for(int i=1;i<=n;i++){JPanel row=new JPanel();row.add(new JLabel("Сезон "+i+": серий"));row.add(new JTextField("1",4));row.add(new JLabel("просмотрено"));row.add(new JTextField("0",4));seasonsRows.add(row);}seasonsRows.revalidate();seasonsRows.repaint();}catch(NumberFormatException ignored){}}
    private Integer intOrNull(String s){return s==null||s.isBlank()?null:Integer.valueOf(s.trim());}
    private void submit(){try{Integer y=intOrNull(year.getText());Integer rt=(Integer)rating.getSelectedItem();SeriesProgress sp=null;if(type.getSelectedItem()==ContentType.SERIES){int n=Integer.parseInt(totalSeasons.getText());int ws=watchedSeasons.getText().isBlank()?0:Integer.parseInt(watchedSeasons.getText());java.util.List<Integer> totals=new ArrayList<>(),watched=new ArrayList<>();for(Component c:seasonsRows.getComponents()){JPanel row=(JPanel)c;JTextField t=(JTextField)row.getComponent(1),w=(JTextField)row.getComponent(3);totals.add(Integer.parseInt(t.getText()));watched.add(Integer.parseInt(w.getText()));}sp=new SeriesProgress(n,totals,ws,watched);}result=new Result(title.getText(),(ContentType)type.getSelectedItem(),y,(Genre)genre.getSelectedItem(),(Status)status.getSelectedItem(),rt,comment.getText(),sp);dispose();}catch(Exception ex){error.setText(ex.getMessage()==null?"Проверьте введенные данные":ex.getMessage());}}
    public Result result(){return result;}
    public void showValidationErrors(ValidationException ex){error.setText(ex.errors().values().stream().findFirst().orElse("Проверьте данные"));}
}
